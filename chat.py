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
        self.groups = []
        self.users = []
        self.user_group_map = {}

    def register_user(self, user_name, user_id, socket):
        user = User(user_name, user_id, socket)

        if not user in self.users:
            self.users.append(user)
            app.logger.info(u'Registered user {}'.format(user_id))
        else:
            app.logger.info('User tried to register with taken id')

    def list_groups(self, socket):
        group_dict = {}

        for group in self.groups:
            group_dict[group.id] = {
                'name': group.name,
                'num_users': group.num_users
            }

        socket.send(serialize(group_dict))

    def list_group_users(self, group_id, socket):
        group = self._get_group_by_id(group_id)

        if not group:
            socket.send(serialize({'error': True, 'message': 'Invalid group ID: {}'.format(group_id)}))

        users = {
            'users': [{'name': u.name, 'id': u.id} for u in group.users]
        }

        socket.send(serialize(users))

    def add_user_to_group(self, user_id, group_id):
        user = self._get_user_by_id(user_id)
        group = self._get_group_by_id(group_id)

        if not user:
            socket.send(serialize({'error': True, 'message': 'Invalid user ID'}))

        if not group:
            group = Group(group_id, 'group_{}'.format(group_id), user)
            self.groups.append(group)
        else:
            group.add_user(user)

        self.user_group_map[user.id] = group

        app.logger.info(u'Added user {} to group {}'.format(user_id, group_id))

    def remove_user_from_group(self, user_id, group_id):
        user = self._get_user_by_id(user_id)
        group = self._get_group_by_id(group_id)

        if not user or not group:
            socket.send(serialize({'error': True, 'message': 'Invalid group or user ID'}))

        group.remove_user(user)
        self.user_group_map[user.id] = None

        app.logger.info(u'Removed user {} from group {} '.format(user_id, group_id))

    def send_message(self, user_id, group_id, message):
        self._clean_dead_users()

        user = self._get_user_by_id(user_id)
        group = self._get_group_by_id(group_id)

        if not user or not group:
            socket.send(serialize({'error': True, 'message': 'Invalid group or user ID'}))

        group.broadcast(user, message)

    def flush_data(self, user_id, socket):
        if user_id == 'sudo':
            self.users = []
            self.groups = []
            self.user_group_map = {}

            app.logger.info(u'Flushed chat users and groups')

            socket.send({'success': True})
        else:
            socket.send({'error': True, 'message': 'Access denied.'})

    def _clean_dead_users(self):
        old_len = len(self.users)

        active_users = [u for u in self.users if u.is_alive]
        dead_users = [u for u in self.users if u not in active_users]
        self.users = active_users

        # Remove the dead users from the groups they're in
        for u in dead_users:
            if u.id in self.user_group_map:
                group = self._get_group_by_id(self.user_group_map[u.id])
                group.remove(u)

                self.user_group_map.pop(u.id)

        if dead_users:
            app.logger.info(u'Cleaned up {} dead connections'.format(len(self.dead_users)))

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
def status_check():
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
            chat.register_user(json_dict.get('name'), json_dict.get('id'), ws)
        elif action == 'list_groups':
            chat.list_groups(ws)
        elif action == 'list_users':
            chat.list_group_users(json_dict.get('group_id'), ws)
        elif action == 'join_group':
            chat.add_user_to_group(json_dict.get('user_id'), json_dict.get('group_id'))
        elif action == 'leave_group':
            chat.remove_user_from_group(json_dict.get('user_id'), json_dict.get('group_id'))
        elif action == 'message':
            chat.send_message(json_dict.get('user_id'), json_dict.get('group_id'), json_dict.get('message'))
        elif action == 'flush':
            chat.flush_data(json_dict.get('user_id'), ws)
        else:
            app.logger.info(u'Got socket message with invalid action: {}'.format(action))
            pass
