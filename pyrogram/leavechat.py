from pyrogram import Client
import sys
import time 

print(sys.argv)
api_id = 1
api_hash = ""

chats = [
int(-1001655084219)
]

with Client("my_account", api_id, api_hash) as app:
    for chat_id in chats:
        try:
            app.leave_chat(chat_id)
            time.sleep(2)
        except FloodWait as e:
            print("FloodWait sleep " + str(e.x) + " after leave_chat\n")
            time.sleep(e.x)
            app.leave_chat(chat_id)

