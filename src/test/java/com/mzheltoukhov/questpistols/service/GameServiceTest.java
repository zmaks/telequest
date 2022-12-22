package com.mzheltoukhov.questpistols.service;

import com.mzheltoukhov.questpistols.model.Game;
import com.mzheltoukhov.questpistols.repository.GameRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private GameService gameService;

    @Test
    public void getGameToInviteByCompetition() throws Exception{
        when(gameRepository.findByCompetitionAndDisabledIsFalse(anyString())).thenReturn(new ArrayList<>(List.of(
                createGame("1", 3), createGame("2", 3), createGame("3", 3),
                createGame("4", 0), createGame("5", 0), createGame("6", 0),
                createGame("7", 0), createGame("8", 0), createGame("9", 0),
                createGame("10", 0), createGame("11", 0), createGame("12", 0)
        )));
        when(gameRepository.save(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArguments()[0]);

        assertEquals("4", gameService.getGameToInviteByCompetition("competition", 3, 3).getName());
    }

    @Test
    public void getGameToInviteByCompetition_2() throws Exception{
        when(gameRepository.findByCompetitionAndDisabledIsFalse(anyString())).thenReturn(new ArrayList<>(List.of(
                createGame("1", 3), createGame("2", 3), createGame("3", 3),
                createGame("4", 2), createGame("5", 2), createGame("6", 2),
                createGame("7", 0), createGame("8", 0), createGame("9", 0),
                createGame("10", 0), createGame("11", 0), createGame("12", 0)
        )));
        when(gameRepository.save(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArguments()[0]);

        assertEquals("4", gameService.getGameToInviteByCompetition("competition", 3, 3).getName());
    }

    @Test
    public void getGameToInviteByCompetition_3() throws Exception{
        when(gameRepository.findByCompetitionAndDisabledIsFalse(anyString())).thenReturn(new ArrayList<>(List.of(
                createGame("1", 2), createGame("2", 2), createGame("3", 2),
                createGame("4", 0), createGame("5", 0), createGame("6", 0),
                createGame("7", 0), createGame("8", 0), createGame("9", 0),
                createGame("10", 0), createGame("11", 0), createGame("12", 0)
        )));
        when(gameRepository.save(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArguments()[0]);

        assertEquals("4", gameService.getGameToInviteByCompetition("competition", 3, 4).getName());
        assertEquals("1", gameService.getGameToInviteByCompetition("competition", 3, 3).getName());
    }

    @Test
    public void getGameToInviteByCompetition_all() throws Exception{
        when(gameRepository.findByCompetitionAndDisabledIsFalse(anyString())).thenReturn(new ArrayList<>(List.of(
                createGame("1", 0), createGame("2", 0), createGame("3", 0),
                createGame("4", 0), createGame("5", 0), createGame("6", 0),
                createGame("7", 0), createGame("8", 0), createGame("9", 0),
                createGame("10", 0), createGame("11", 0), createGame("12", 0)
        )));
        when(gameRepository.save(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArguments()[0]);

        assertEquals("1", gameService.getGameToInviteByCompetition("competition", 2, 3).getName());
        assertEquals("2", gameService.getGameToInviteByCompetition("competition", 2, 3).getName());
        assertEquals("3", gameService.getGameToInviteByCompetition("competition", 2, 3).getName());
        assertEquals("3", gameService.getGameToInviteByCompetition("competition", 2, 3).getName());
        assertEquals("2", gameService.getGameToInviteByCompetition("competition", 2, 3).getName());
        assertEquals("1", gameService.getGameToInviteByCompetition("competition", 2, 3).getName());
        assertEquals("4", gameService.getGameToInviteByCompetition("competition", 2, 3).getName());
        assertEquals("5", gameService.getGameToInviteByCompetition("competition", 2, 3).getName());
        assertEquals("6", gameService.getGameToInviteByCompetition("competition", 2, 3).getName());
        assertEquals("6", gameService.getGameToInviteByCompetition("competition", 2, 3).getName());
        assertEquals("5", gameService.getGameToInviteByCompetition("competition", 2, 3).getName());
        assertEquals("4", gameService.getGameToInviteByCompetition("competition", 2, 3).getName());
    }

    @Test
    public void getGameToInviteByCompetition_all_batch1() throws Exception{
        when(gameRepository.findByCompetitionAndDisabledIsFalse(anyString())).thenReturn(new ArrayList<>(List.of(
                createGame("1", 0), createGame("2", 0), createGame("3", 0),
                createGame("4", 0), createGame("5", 0), createGame("6", 0),
                createGame("7", 0), createGame("8", 0), createGame("9", 0),
                createGame("10", 0), createGame("11", 0), createGame("12", 0)
        )));
        when(gameRepository.save(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArguments()[0]);

        assertEquals("1", gameService.getGameToInviteByCompetition("competition", 2, 1).getName());
        assertEquals("1", gameService.getGameToInviteByCompetition("competition", 2, 1).getName());
        assertEquals("2", gameService.getGameToInviteByCompetition("competition", 2, 1).getName());
        assertEquals("2", gameService.getGameToInviteByCompetition("competition", 2, 1).getName());
        assertEquals("3", gameService.getGameToInviteByCompetition("competition", 2, 1).getName());
        assertEquals("3", gameService.getGameToInviteByCompetition("competition", 2, 1).getName());
        assertEquals("4", gameService.getGameToInviteByCompetition("competition", 2, 1).getName());
        assertEquals("4", gameService.getGameToInviteByCompetition("competition", 2, 1).getName());
    }

    @Test
    public void getGameToInviteByCompetition_all_batchFull() throws Exception{
        when(gameRepository.findByCompetitionAndDisabledIsFalse(anyString())).thenReturn(new ArrayList<>(List.of(
                createGame("1", 0), createGame("2", 0), createGame("3", 0),
                createGame("4", 0), createGame("5", 0), createGame("6", 0)
        )));
        when(gameRepository.save(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArguments()[0]);

        assertEquals("1", gameService.getGameToInviteByCompetition("competition", 2, 6).getName());
        assertEquals("2", gameService.getGameToInviteByCompetition("competition", 2, 6).getName());
        assertEquals("3", gameService.getGameToInviteByCompetition("competition", 2, 6).getName());
        assertEquals("4", gameService.getGameToInviteByCompetition("competition", 2, 6).getName());
        assertEquals("5", gameService.getGameToInviteByCompetition("competition", 2, 6).getName());
        assertEquals("6", gameService.getGameToInviteByCompetition("competition", 2, 6).getName());
        assertEquals("6", gameService.getGameToInviteByCompetition("competition", 2, 6).getName());
        assertEquals("5", gameService.getGameToInviteByCompetition("competition", 2, 6).getName());
    }

    private Game createGame(String name, int invites) {
        Game game = new Game();
        game.setInvites(invites);
        game.setName(name);
        return game;
    }
}
