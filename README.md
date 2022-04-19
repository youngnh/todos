To start an nrepl server:

> clj -M:dev

To start a standalone todo api server:

> clj -M:server

# API

- GET /{user}/tasks
View list of existing todo tasks

returns:
```
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
on success:
```
{
  status: "ok"
  id: "taskid"
}
```

on failure:
```
{
  status: "error"
  error: "error message"
}
```

- POST /{user}/tasks/{id}/toggle
on success:
```
{
  status: "ok"
  id: "taskid"
}
```

on failure:
```
{
  status: "error"
  error: "error message"
}
```

- DELETE /{user}/tasks/{id}
on success:
```
204 No Content
```

- GET /{user}/charts/progress
```
{
  "complete": 5
  "incomplete": 2
}
```

- GET /{user}/charts/burndown
params:
```
start=2022-04-02
end=2022-04-01
span=1d
```

```
{
  "completion": [
    { "2022-04-01": 1 }
    { "2022-04-02": 2 }
  ],
  "creation": [
    { "2022-04-01": 5 }
    { "2022-04-02": 3 }
  ]
}
