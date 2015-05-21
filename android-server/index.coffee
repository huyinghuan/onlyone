###<reference path="typings/node/node-0.10.d.ts"/>###
app =  require('express')();
server = require('http').createServer(app)
io = require('socket.io')(server)

app.get('/', (req, resp)->
    resp.sendFile(__dirname + '/index.html')
)

chartNsp = io.of('/chat')
chartNsp.on("connection", (socket)->
    socket.on("login", (IMSI, device_id)->
        console.log "user: ", IMSI, device_id
        socket.emit('login:success', "ok")
        socket.join("test")
    )
    socket.on('message', (data)->
       socket.to("test").emit("message", data)
    )
)
chartNsp.on("disconnection", (socket)->
    socket.leave("test")
)


server.listen(3000)