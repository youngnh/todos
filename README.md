To start an nrepl server:

> clj -M:dev

To start a standalone todo api server:

> clj -M:server

# API

- GET /tasks
View list of existing todo tasks

returns:
```
{
  id: "taskid"
  name: "Name of task"
}
```

- POST /tasks
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

- POST /tasks/{id}/toggle
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

- DELETE /tasks/{id}
on success:
```
{
  status: "ok"
  id: "taskid"
}
```

- GET /charts/progress
```
{
  "complete": 5
  "incomplete": 2
}
```

- GET /charts/burndown
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
