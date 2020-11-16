let stompClient = null;
let subscriptionUrl = $("meta[name=x_data_subscription_url]").attr("content");
let startPostUrl = $("meta[name=x_data_start_post_url]").attr("content");


function connect() {
    let socket = new SockJS('/websocket');
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

function postForStartingGame(){
    $.ajax({
        type : "POST",
        url : startPostUrl,
        contentType : "application/json",
        dataType : "JSON"
    }).done(function() {
        location.reload();
    }).fail(function() {
        alert("スタートに失敗しました。ページをリロードしてもう一度ゲームをスタートさせてください");
    });

}

$(function () {
    let scrollTopId = '#top';
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

    let startGame = $('#startGame');
    startGame.on('click', 'button', function(){
        postForStartingGame();
    })

    connect();
});
