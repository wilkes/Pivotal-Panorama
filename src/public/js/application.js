$(document).ready(function() {
  $('.card-title').click(function() {
    $(this).nextAll('.card-body, .card-footer').slideToggle("normal");
  });

  $('form').submit(function () {
    var iteration = $(':input#iteration')
    this.action = "/" + iteration.val();
    // Remove name so that it won't be sent as a parameter int the url
    iteration.removeAttr('name'); 
  }) 
 
});