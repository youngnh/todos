To start an nrepl server:

> clj -M:dev

To start a standalone todo api server:

> clj -M:server

To run the tests:

> clj -X:dev:test

# Notes to reviewers

I'm just about at 3 hours spent on this, and I think I'm going to wrap things up here. A few things didn't quite get done:
- better connection management: ensuring that a database connection gets closed properly during the course of a request's lifetime
- more tests around the database layer: toggling task completion, progress & burndown chart behavior...
- api-level / ring handler tests (though the [PAW](https://paw.cloud/) requests and the api outline below help a bit here)

but hopefully this assignment demonstrates that I'm comfortable writing clojure and can do so in a clean, maintainable manner.

# API

- POST /login
params:
```
{ "user": "alice@example.com" }
```

returns:
```
201 Created
{
  "status": "ok",
  "token": "<auth token>"
}
```
when interacting with the api, all requests should have an `Authorization` header with the value `Bearer <auth token>` obtained from this api endpoint

- GET /{user}/tasks
View list of existing todo tasks

returns:
```
200 OK
{
  "id": "taskid"
  "name": "Name of task"
  "created": 1650384918
  "completed": 1650385453
}
```
where `created` and `completed` are [unix epoch time](https://en.wikipedia.org/wiki/Unix_time) and `completed` will be `null` if the task is has not been marked complete.

- POST /{user}/tasks
params:
```
{ "name": "Name of task" }
```

returns:
```
201 Created
{
  "status": "ok"
  "id": "taskid"
}
```

- POST /{user}/tasks/{id}/toggle
```
201 Created
{
  "status": "ok"
  "id": "taskid"
}
```

- DELETE /{user}/tasks/{id}
```
204 No Content
```

- GET /{user}/charts/progress
```
200 OK
{
  "complete": 5
  "incomplete": 2
}
```

- GET /{user}/charts/burndown
```
200 OK
{
  "completed": [
    {
      "completed": 1650376783,
      "count": 1
    },
    {
      "completed": 1650376793,
      "count": 2
    }
  ],
  "created": [
    {
      "created": 1650335529,
      "count": 1
    },
    {
      "created": 1650375743,
      "count": 3
    }
  ]
}
```
