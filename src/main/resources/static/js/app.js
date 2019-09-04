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
    var scrollTopId = '#top';
    if ($('.scrollTopCandidate').length > 0 && $('.scrollTopCandidate').length < 3 ) {
        scrollTopId = '#' + $('.scrollTopCandidate')[0].id;
    } else if ($('.scrollTopCandidate').length >= 3 && $('.scrollTopCandidate').length < 10 ) {
        scrollTopId = '#' + $('.scrollTopCandidate')[2].id;
    } else if ($('.scrollTopCandidate').length >= 10) {
        scrollTopId = '#' + $('.scrollTopCandidate')[8].id;
    }
    if ($(scrollTopId).length > 0) {
        $("html,body").animate({scrollTop:$(scrollTopId).offset().top});
    }
    $('[data-toggle="tooltip"]').tooltip();
    connect();
});
