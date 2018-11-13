$(".navbar-search-input").mouseup(function () {
    $(this).animate({ width: '300px' }, 500);
});
$(".navbar-search-input").focusout(function () {
    if($(".navbar-search-input").val() === ""){
        $(this).animate({ width: '100px' }, 500);
    }
});
$('.navbar-search-input').keydown(function(event) {
    if (event.keyCode == 13) {
        console.log("你输入的值是" + $('.navbar-search-input').val());
        $('.navbar-search-input').val("");
        $('.navbar-search-input').focusout();
    }
});