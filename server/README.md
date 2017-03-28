## Server app for the IM project

### Example Requests/Responses that can be sent/received to/fom the server
***

#### Register new user
Request:
```javascript
{
    "action": "register",
    "user_id": "prateek"
}
```

Response:
```javascript
{
    "type": "ack"
}
```
***

#### List groups
Request:
```javascript
{
    "action": "list_groups"
}
```

Response:
```javascript
{
    "type": "list_groups",
    "groups": [
        {
            "group_id": "Cool Kids Group",
            "num_users": 50
        },
        {
            "group_id": "Cooler Kids Group",
            "num_users": 12
        },
        ,
        {
            "group_id": "Coolest Kids Group",
            "num_users": 0
        }
    ]
}
```
***

#### Join group
Request:
```javascript
{
    "action": "join_group",
    "group_id": "Cool Kids Group",
    "user_id": "prateek"
}
```

Response:
```javascript
{
    "type": "ack"
}
```
***

#### Message a group
Request:
```javascript
{
    "action": "message",
    "group_id": "Cool Kids Group",
    "message": "Greetings, fellow cool kids",
    "user_id": "prateek"
}
```

Response:
```javascript
{
    "type": "ack"
}
```
***

#### Leave group
Request:
```javascript
{
    "action": "leave_group",
    "group_id": "Cool Kids Group",
    "user_id": "prateek"
}
```

Response:
```javascript
{
    "type": "ack"
}
```
***

#### Reset groups (to the initial 5) and users (no users)
Request:
```javascript
{
    "action": "flush"
}
```

Response:
```javascript
{
    "type": "ack"
}
```
***

#### Error responses
```javascript
{
    "type": "error",
    "message": "Invalid user ID or group ID"
}