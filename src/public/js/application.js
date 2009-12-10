
$(document).ready(function() {
  $('form').submit(function () {
    var iteration = $(':input#iteration').val();
    var group_by = $(':input#group-by').val();
    var url = "/group" + "/" + iteration + "/" + group_by;
    console.log(url);
    window.location = url;
    return false;
  })
});