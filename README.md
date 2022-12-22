## How to run the bot
1. Install docker and docker-compose (use [this script](install/install-docker.sh) in case of ubuntu)
2. Declare your quest in json format (see [example](src/main/resources/quests/demo.json))
3. Specify env variable for the `questols` service in the [docker-compose](docker-compose.yml) file
   1. `TELEGRAM_TOKEN` - the token of your telegram bot
   2. `QUEST_DEFAULT_NAME` - the name of your quest
   3. `BOT_ADMINS` - the **admin** username in telegram (the user who can setup the bot)
4. Run the docker-compose - `docker-compose up -d`

---
## How to launch a game
1. Send the quest json file to your bot in DM from the **admin** account
2. Upload requested media files as replies to each file request message from the bot
3. Send `/create <game-name>` command to bot where `<game-name>` is the current game name
4. Invite the bot to the team group chats to add the team to the game
5. Send `/sendhello <game-name>` to greet the teams before starting the game (`startMessage` from the quest declaration will be sent)
6. Send `/run <game-name>` to run start the game
### Useful admin commands:
- `/status <game-name>` - get the game stats
- `/report <game-name>` - get the game report xls file (all stats by all game tasks and teams)
---

# Refactoring plan
**1. Move all system game messages to the quest declaration** 

The code contains raw 'hardcoded strings' (e.g. some command replies) that should not be there and should be configurable via quest declaration.

**2. Apply the chain-of-responsibility GOF pattern for the task processing logic**

Current task processor that are responsible to check the user's answer (such as the main [SimpleQuestionTaskProcessor](src/main/java/com/mzheltoukhov/questpistols/service/processor/SimpleQuestionTaskProcessor.java) 
concentrate a lot of business logic that made the code hard to read and maintain. The idea is to wrap each
check/decision/state-change to separate chain link (filter) and then combine these links into chains. 
Each task type should have their own chains where links can be reusable. The chains should be configured in configuration classes.
For example, the chain may look like this using a chain builder: `chain.init().then(AnswerFormatFilter.class).then(AnswerCorrectnessFilter.class).then(PointCalcFilter.class)...` or even in declarative way in application.yml.
Each chain link (filter) has a context object that that stores results of each link.

**3. Separate the messenger logic from the core game logic**

Core game logic should be independent of the user communication logic (Telegram Bot API).
Ideally they should split into different services and communicate with each other via a message broker. This solves two
crucial issues: 1. other messenger platforms (Facebook, Slack) or WEB app can be used to communicate with users. 2. Message platform service will 
be responsible for its timeouts and RPS which will allow to eliminate all the `sleep(...)` invocations in the code.

**4. Integrate RateLimiter and Queue buffers into messenger services**

Messengers have RPS restrictions that the app should follow. RateLimiter should restrict 'users' that produce unnatural load.
Queue buffers should provide smooth messenger API usage to deal with the RPS restrictions.

**5. Apply the command GOF pattern for the Telegram commands**

Each command logic (e.g. `/create`, `/run`, `/status`) should be separated to different components that follow the Command pattern.
They should not be in one place controlled by `if` closures.

**6. Add bulk media files uploading**

Currently, all media files for quests are upload one by one which takes time. Implement bulk uploading using zip archives of the files.

**7. Increase code test coverage**

Add more tests for the core logic

**8. Add automation tests**

Implement automation testing framework using Pyrogram to run quest end2end test in Telegram.

**9. Migrate to Spring Boot 3 and native images**

**10. Add ELK and monitoring**