class Group:

    def __init__(self, id, name, user):
        self.name = name
        self.id = id
        self.users = [user]

    @property
    def num_users(self):
        return len(self.users)

    def broadcast(self, from_user, message):
        for user in self.users:
            if user != from_user:
                user.send_message(self, from_user, message)

    def has_user(self, user):
        return user in self.users

    def add_user(self, user):
        if not self.has_user(user):
            self.users.append(user)

    def remove_user(self, user):
        if self.has_user(user):
            self.users.remove(user)
