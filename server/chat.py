from __future__ import absolute_import

from flask import Flask
from flask_sockets import Sockets

from models.group import Group
from models.user import User
from utils import serialize, deserialize

app = Flask(__name__)
app.debug = True

sockets = Sockets(app)

class ChatService(object):

    def __init__(self):
        self.groups = [Group('Group {}'.format(i)) for i in range(5)]
        self.users = []
        self.user_group_map = {}

    def register_user(self, user_id, socket):
        user = User(user_id, socket)

        if not user in self.users:
            self.users.append(user)
            app.logger.info(u'Registered user {}'.format(user_id))
        else:
            # TODO: Overwrite current user with new socket
            app.logger.info('User tried to register with taken id')

    def list_groups(self, socket):
        groups = []

        for group in self.groups:
            groups.append({
                'group_id': group.id,
                'num_users': group.num_users
            })

        socket.send(serialize({'type': 'list_groups', 'groups': groups}))

    def list_group_users(self, group_id, socket):
        group = self._get_group_by_id(group_id)

        if not group:
            socket.send(serialize({'type': 'error', 'message': 'Invalid group ID: {}'.format(group_id)}))

        users = []
        for user in group.users:
            users.append({
                'user_id': user.id
            })

        socket.send(serialize({'type': 'list_group_users', 'users': users}))

    def add_user_to_group(self, user_id, group_id, socket):
        user = self._get_user_by_id(user_id)
        group = self._get_group_by_id(group_id)

        if not user:
            socket.send(serialize({'type': 'error', 'message': 'Invalid user ID'}))

        if not group:
            group = Group(group_id)
            self.groups.append(group)

        added = group.add_user(user)

        if user.id in self.user_group_map and added:
            self.user_group_map[user.id].append(group)
        elif added:
            self.user_group_map[user.id] = [group]

        if added:
            app.logger.info(u'Added user {} to group {}'.format(user_id, group_id))
        else:
            app.logger.info(u'Tried to add user {} to group {} but they already in there lmfaoo'.format(user_id, group_id))

    def remove_user_from_group(self, user_id, group_id, socket):
        user = self._get_user_by_id(user_id)
        group = self._get_group_by_id(group_id)

        if not user or not group:
            socket.send(serialize({'type': 'error', 'message': 'Invalid group or user ID'}))

        # This will be False if the user wasn't in the group to begin with
        removed = group.remove_user(user)

        if removed:
            self.user_group_map[user.id].remove(group)
            app.logger.info(u'Removed user {} from group {}'.format(user_id, group_id))
        else:
            app.logger.info(u'Tried to remove user {} from group {}, but they werent even in there xD'.format(user_id, group_id))

    def send_message(self, user_id, group_id, message, socket):
        self._clean_dead_users()

        user = self._get_user_by_id(user_id)
        group = self._get_group_by_id(group_id)

        if not user or not group:
            app.logger.info(u'User {} not in group {} '.format(user_id, group_id))
            socket.send(serialize({'type': 'error', 'message': 'Invalid group or user ID'}))
        else:
            group.broadcast(user, message)
            app.logger.info(u'User {} messaged {} to group {}'.format(user_id, message, group_id))

    def flush_data(self, user_id, socket):
        self.users = []
        self.groups = [Group('Group {}'.format(i), None) for i in range(5)]
        self.user_group_map = {}

        app.logger.info(u'Flushed chat users and groups')

    def _clean_dead_users(self):
        old_len = len(self.users)

        active_users = [u for u in self.users if u.is_alive]
        dead_users = [u for u in self.users if u not in active_users]
        self.users = active_users

        # Remove the dead users from the groups they're in
        for u in dead_users:
            if u.id in self.user_group_map:
                if self.user_group_map[u.id]:
                    for group in self.user_group_map[u.id]:
                        self._get_group_by_id(group.id).remove_user(u)

                self.user_group_map.pop(u.id)

        if dead_users:
            app.logger.info(u'Cleaned up {} dead connections'.format(len(dead_users)))
            for u in dead_users:
               app.logger.info(u'Cleaned up {}'.format(u.id))

    def _get_group_by_id(self, group_id):
        for g in self.groups:
            if g.id == group_id:
                return g
        return None

    def _get_user_by_id(self, user_id):
        for u in self.users:
            if u.id == user_id:
                return u
        return None

chat = ChatService()


@app.route('/', methods=['GET'])
def hello_world():
    return 'Hello World', 200


@app.route('/status', methods=['GET'])
def status_check():
    return 'Success', 200


@sockets.route('/')
def socket_in_handler(ws):
    while not ws.closed:
        message = ws.receive()

        if not message:
            return

        json_dict = deserialize(message)
        action = json_dict.get('action')

        if action == 'register':
            chat.register_user(json_dict.get('user_id'), ws)
            _send_ack(ws)
        elif action == 'keep_alive':
            _send_ack(ws)
        elif action == 'list_groups':
            chat.list_groups(ws)
        elif action == 'list_group_users':
            chat.list_group_users(json_dict.get('group_id'), ws)
        elif action == 'join_group':
            chat.add_user_to_group(json_dict.get('user_id'), json_dict.get('group_id'), ws)
            _send_ack(ws)
        elif action == 'leave_group':
            chat.remove_user_from_group(json_dict.get('user_id'), json_dict.get('group_id'), ws)
            _send_ack(ws)
        elif action == 'message':
            chat.send_message(json_dict.get('user_id'), json_dict.get('group_id'), json_dict.get('message'), ws)
        elif action == 'flush':
            chat.flush_data(json_dict.get('user_id'), ws)
            _send_ack(ws)
        else:
            app.logger.info(u'Got socket message with invalid action: {}'.format(action))
            pass


def _send_ack(socket):
    socket.send(serialize({'type': 'ack'}))

# Uncomment the following to run locally and run 'python chat.py' from the root folder
# if __name__ == "__main__":
#     from gevent import pywsgi
#     from geventwebsocket.handler import WebSocketHandler
#     server = pywsgi.WSGIServer(('', 5000), app, handler_class=WebSocketHandler)
#     server.serve_forever()

