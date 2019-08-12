var stompClient = null;
var subscriptionUrl = $("meta[name=x_data_subscription_url]").attr("content");

function connect() {
    var socket = new SockJS('/websocket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe(subscriptionUrl, function (greeting) {
            disconnect();
            location.reload();
        });
    });
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    console.log("Disconnected");
}


$(function () {
    $('[data-toggle="tooltip"]').tooltip();
    connect();
});
