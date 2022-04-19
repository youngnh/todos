To start an nrepl server:

> clj -M:dev

To start a standalone todo api server:

> clj -M:server

# API

- GET /{user}/tasks
View list of existing todo tasks

returns:
```
200 OK
{
  id: "taskid"
  name: "Name of task"
  created: 1650384918
  completed: 1650385453
}
```
where `created` and `completed` are [unix epoch time](https://en.wikipedia.org/wiki/Unix_time) and `completed` will be `null` if the task is has not been marked complete.

- POST /{user}/tasks
params:
```
{ name: "Name of task" }
```

returns:
```
201 Created
{
  status: "ok"
  id: "taskid"
}
```

- POST /{user}/tasks/{id}/toggle
```
201 Created
{
  status: "ok"
  id: "taskid"
}
```

- DELETE /{user}/tasks/{id}
on success:
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
