from pyrogram import Client
from pyrogram.types import ChatPermissions
from pyrogram.errors import FloodWait
import sys
import time 

print(sys.argv)
bot_username = "RoomsQuizBot"
api_id = 1
api_hash = ""
chat_perfix = "Игра ROOMS"
chat_count = int(10)
start_number = int(21)
game_name = "s"

print("sleep 10 min")
time.sleep(600)

with Client("my_account", api_id, api_hash) as app:
    photo_message = app.send_photo("me", "rooms-team.jpeg")
    logo_file_id = photo_message.photo.file_id
    app.delete_messages("me", photo_message.message_id)
    for x in range(start_number, start_number + chat_count):
        try:
            chat = app.create_supergroup(chat_perfix + " " + str(x))
            time.sleep(3)
        except FloodWait as e:
            print("FloodWait sleep " + str(e.x) + " after create_supergroup\n")
            time.sleep(e.x)
#         except Exception as e:
#             print("EXCEPTION create_supergroup" + str(e) + "\n")



        try:
            app.set_chat_permissions(chat.id, ChatPermissions(can_change_info=False, can_send_messages=True, can_send_media_messages=True, can_send_stickers=True, can_send_animations=True, can_invite_users=False, can_pin_messages=False, can_add_web_page_previews=True))
            time.sleep(3)
        except FloodWait as e:
            print("FloodWait sleep " + str(e.x) + " after set_chat_permissions\n")
            time.sleep(e.x)
#         except Exception as e:
#             print("EXCEPTION set_chat_permissions" + str(e) + "\n")

#         try:
#             app.set_slow_mode(chat.id, 10)
#             time.sleep(3)
#         except FloodWait as e:
#             time.sleep(e.x)
#             print("FloodWait sleep " + str(e.x) + " after set_slow_mode\n")
#         except Exception as e:
#             print("EXCEPTION set_slow_mode" + str(e) + "\n")

        try:
            chat.add_members(bot_username)
            time.sleep(3)
        except FloodWait as e:
            print("FloodWait sleep " + str(e.x) + " after add_members\n")
            time.sleep(e.x)
#         except Exception as e:
#             print("EXCEPTION add_members" + str(e) + "\n")

        try:
            chat.promote_member(bot_username, can_manage_chat=True, can_change_info=True, can_post_messages=True, can_edit_messages=True, can_delete_messages=True, can_restrict_members=True, can_invite_users=True, can_pin_messages=True, can_promote_members=True)
            time.sleep(3)
        except FloodWait as e:
            print("FloodWait sleep " + str(e.x) + " after promote_member\n")
            time.sleep(e.x)
#         except Exception as e:
#             print("EXCEPTION promote_member" + str(e) + "\n")

        try:
            m = app.send_message(chat.id, "/add " + game_name)
            app.delete_messages(chat.id, m.message_id)
            time.sleep(3)
        except FloodWait as e:
            print("FloodWait sleep " + str(e.x) + " after delete_messages\n")
            time.sleep(e.x)
#         except Exception as e:
#             print("EXCEPTION delete_messages" + str(e) + "\n")

        try:
            link = app.export_chat_invite_link(chat.id)
            print(str(chat.id) + " " + str(chat.title) + " " + str(link) + "\n")
            time.sleep(3)
        except FloodWait as e:
            print("FloodWait sleep " + str(e.x) + " after export_chat_invite_link\n")
            time.sleep(e.x)
#         except Exception as e:
#             print("EXCEPTION export_chat_invite_link" + str(e) + "\n")

        try:
            app.set_chat_photo(chat.id, photo=logo_file_id)
            time.sleep(3)
        except FloodWait as e:
            print("FloodWait sleep " + str(e.x) + " after set_chat_photo\n")
            time.sleep(e.x)
            app.set_chat_photo(chat.id, photo=logo_file_id)
#         except Exception as e:
#             print("EXCEPTION set_chat_photo" + str(e) + "\n")
    time.sleep(90)

