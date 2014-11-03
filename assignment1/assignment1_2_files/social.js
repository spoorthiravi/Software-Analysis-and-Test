function loadSocialButtons() {
    var d = document.getElementById('social')
    if (d != null) {
        var url = d.getAttribute('href', 0);
        if (url == null) url = location.href;
        //d.innerHTML = d.innerHTML
        //  + "<iframe src='//www.facebook.com/plugins/like.php?href=" + encodeURI(url) + "&amp;layout=button_count&amp;show_faces=false&amp;width=100&amp;action=like&amp;font=segoe+ui&amp;colorscheme=light&amp;height=21' scrolling='no' frameborder='0' style='border:none; overflow:hidden; width:80px; height:21px;' allowTransparency='true'></iframe>"
        //  + "<a href=\"http://www.reddit.com/submit\" onclick=\"window.location = 'http://www.reddit.com/submit?url='" + encodeURIComponent(url) + "\"> <img src=\"http://www.reddit.com/static/spreddit7.gif\" style=\"margin-bottom:2px;\" alt=\"submit to reddit\" border=\"0\" /></a>";
    }
}
loadSocialButtons();
