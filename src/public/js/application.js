$(document).ready(function() {
  $('form').submit(function () {
    var iteration = $(':input#iteration')
    this.action = "/" + iteration.val();
    // Remove name so that it won't be sent as a parameter int the url
    iteration.removeAttr('name'); 
  })  
});