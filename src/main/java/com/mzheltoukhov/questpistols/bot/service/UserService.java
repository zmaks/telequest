package com.mzheltoukhov.questpistols.bot.service;

import com.mzheltoukhov.questpistols.bot.model.BotUser;
import com.mzheltoukhov.questpistols.bot.model.RegistrationResult;
import com.mzheltoukhov.questpistols.exception.IncorrectCodePhraseException;
import com.mzheltoukhov.questpistols.exception.RegistrationException;
import com.mzheltoukhov.questpistols.model.Competition;
import com.mzheltoukhov.questpistols.model.Invite;
import com.mzheltoukhov.questpistols.repository.BotUserRepository;
import com.mzheltoukhov.questpistols.service.CompetitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.OffsetDateTime;

@Service
@Slf4j
public class UserService {

    private final BotUserRepository botUserRepository;
    private final CompetitionService competitionService;

    public UserService(BotUserRepository botUserRepository, CompetitionService competitionService) {
        this.botUserRepository = botUserRepository;
        this.competitionService = competitionService;
    }

    public RegistrationResult register(Message message) {
        if (!message.hasText()) {
            throw new IllegalStateException("Message has no text");
        }
        String messageText = message.getText().toLowerCase();
        BotUser user = botUserRepository.findByUserId(message.getFrom().getId());

        if (user != null) {
            if (user.getAttempts() >= 10) {
                log.info("User {} registration rejected due to many attempts", user.getName());
                return RegistrationResult.builder()
                        .message("Вы сделали слишком много попыток")
                        .build();
            }
            if (user.getRegistrationCancellations() >= 3) {
                log.info("User {} registration rejected due to limit of re-registration", user.getName());
                return RegistrationResult.builder()
                        .message("Лимит попыток регистрации превышен. Обратитесь к администратору.")
                        .build();
            }
            user.setLastMessage(messageText);
            user.setLastMessageAt(OffsetDateTime.now());
            botUserRepository.save(user);
            if (user.getRegisteredGame() == null) {
                try {
                    Invite invite = competitionService.getGameInvite(messageText);

                    if (invite.getCompetition().getStatus() == Competition.Status.STARTED
                            && user.getRegistrationCancellations() > 0) {
                        competitionService.cancelGameInvite(invite);
                        log.info("User {} reregistration rejected due to competition already started", user.getName());
                        return RegistrationResult.builder()
                                .message("Менять команду после старта игры запрещено. Если это произошло по ошибке, обратитесь к администратору.")
                                .build();
                    }

                    user.setRegisteredGame(invite.getGameName());
                    user.setRegisteredAt(OffsetDateTime.now());
                    user.setErrorMessage(null);
                    user.setAttempts(user.getAttempts() + 1);
                    botUserRepository.save(user);
                    log.info("User {} REGISTERED to game {} by code {}", user.getName(), invite.getGameName(), messageText);
                    return RegistrationResult.builder()
                            .invitedToChatId(invite.getInvitedToChat())
                            .registered(true)
                            .targetUser(user.getName())
                            .targetUserId(user.getUserId())
                            .gameName(invite.getGameName())
                            .message("Вы зарегистрированы в команду *" + invite.getGameName() + "*!")
                            .registrationCodeMessage("Вот код регистрации вашей команды: *" + invite.getRegistrationCode() + "*\nНезарегистрированные игроки могут указать этот код вместо кодовой фразы игры при регистрации, чтобы попасть к вам в команду.")
                            .build();
                } catch (IncorrectCodePhraseException e) {
                    user.setAttempts(user.getAttempts() + 1);
                    String errorMessage = "Неправильное кодовое слово";
                    user.setErrorMessage(errorMessage);
                    botUserRepository.save(user);
                    log.info("User {} registration rejected due to limit of re-registration", user.getName());
                    return RegistrationResult.builder().message(errorMessage).build();
                } catch (RegistrationException e) {
                    String errorMessage = getErrorMessage(e);
                    user.setErrorMessage(errorMessage);
                    botUserRepository.save(user);
                    log.info("User {} registration rejected {}", user.getName(), e.getMessageType());

                    return RegistrationResult.builder().message(errorMessage).build();
                }
            } else {
                log.info("User {} registration rejected. Already registered.", user.getName());
                return RegistrationResult.builder().message("Вы уже зарегистрированы").build();
            }
        } else {
            user = new BotUser(message.getFrom());
            user.setLastMessageAt(OffsetDateTime.now());
            user.setLastMessage(messageText);
            botUserRepository.save(user);
            log.info("User {} ({}) added to DB}", user.getName(), user.getUserId());
        }
        log.info("User {} has been added and welcomed", user.getName());
        return RegistrationResult.builder().message("Добро пожаловать в игру *ROOMS*! \uD83D\uDC4B" +
                "\n\n Чтобы зарегистрироваться, пришлите кодовое слово/фразу игры, и я определю вас в команду." +
                "\nЕсли вы знаете, в какую команду хотите попасть, то пришлите четырехзначный код команды.")
                .build();
    }

    private String getErrorMessage(RegistrationException e) {
        if (e.getMessageType() == RegistrationException.MessageType.COMPETITION_IS_FULL) {
            return "К сожалению, все места на игру заняты ☹️\nОбратитесь к администраторам, они вам помогут.";
        }
        if (e.getMessageType() == RegistrationException.MessageType.GAME_IS_FULL) {
            return "К сожалению, в этой команде все места заняты ☹️\nВы можете или ввести кодовое слово игры, и я сам определю вас в команду, или ввести код другой команды." +
                    "\nИли же обратитесь к администраторам, они вам помогут.";
        }
        if (e.getMessageType() == RegistrationException.MessageType.GAME_NOT_STARTED_IN_COMPETITION) {
            return "Игра еще не запущена. Обратитесь к администратору.";
        }
        return "Ошибка";
    }

    public void cancelRegistration(Long userId, boolean useAttempt) {
        BotUser user = botUserRepository.findByUserId(userId);

        if (user != null) {
            log.info("User {} ({} {}) canceled registration from game {}",
                    user.getName(), user.getUsername(), user.getUserId(), user.getRegisteredGame());
            user.setRegisteredAt(null);
            user.setRegisteredGame(null);
            user.setErrorMessage(null);
            if (useAttempt) {
                user.setRegistrationCancellations(user.getRegistrationCancellations() + 1);
            }
            botUserRepository.save(user);
        }
    }

    public void setInviteMessageId(Long userId, Integer messageId) {
        BotUser user = botUserRepository.findByUserId(userId);
        user.setInviteMessageId(messageId);
        botUserRepository.save(user);
    }

    public Integer getInviteMessageId(Long userChatWithBotId) {
        BotUser user = botUserRepository.findByUserId(userChatWithBotId);
        if (user != null) {
            return user.getInviteMessageId();
        }
        return null;
    }
}
