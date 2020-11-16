let stompClient = null;
let subscriptionUrl = $("meta[name=x_data_subscription_url]").attr("content");
let startPostUrl = $("meta[name=x_data_start_post_url]").attr("content");
let token = $("meta[name='_csrf']").attr("content");
let header = $("meta[name='_csrf_header']").attr("content");

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
        url : startPostUrl
    }).done(function() {
        location.reload();
    }).fail(function(jqXHR, textStatus, errorThrown){
        console.log("jqXHR          : " + jqXHR.status); // HTTPステータスを表示
        console.log("textStatus     : " + textStatus);    // タイムアウト、パースエラーなどのエラー情報を表示
        console.log("errorThrown    : " + errorThrown.message);
        alert("スタートに失敗しました。ページをリロードしてもう一度ゲームをスタートさせてください");
    }).always(function() {
        console.log('complete');
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

    $(document).ajaxSend(function(e, xhr, options) {
        xhr.setRequestHeader(header, token);
    });

    let startGame = $('#startGame');
    startGame.on('click', function(e){
        e.preventDefault();
        postForStartingGame();
    })

    connect();
});
