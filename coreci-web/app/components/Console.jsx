// See http://en.wikipedia.org/wiki/ANSI_escape_code

var React = require('react');

var regex = /\u001b\[(\d{1,2}(;\d{1,2})*)m/g;
var colorTable = ['black', 'red', 'green', 'yellow', 'blue', 'magenta', 'cyan', 'white'];

var convertColorsAnsiToHtml = function (raw) {
  var fgColor = null;
  var bgColor = null;

  var inner = raw.replace(regex, function (all, numbersString) {
    var numbers = numbersString.split(';').map(s => parseInt(s, 10));

    numbers.forEach(number => {
      if (number == 0) {
        fgColor = null;
        bgColor = null;
      } else if (number >= 30 && number < 38) {
        fgColor = colorTable[number - 30];
      } else if (number >= 40 && number < 48) {
        bgColor = colorTable[number - 40];
      } else if (number >= 90 && number < 98) {
        fgColor = colorTable[number - 90];
      } else if (number >= 100 && number < 108) {
        bgColor = colorTable[number - 100];
      }
    });

    var classes = [];
    if (fgColor) classes.push('fg-' + fgColor);
    if (bgColor) classes.push('bg-' + bgColor);
    classes = classes.filter(c => c).map(c => 'console-output-' + c).join(' ');

    return '</span><span class="' + classes + '">';
  });

  return '<span class="">' + inner + '</span>';
};

var Console = React.createClass({
  render: function () {
    var colored = convertColorsAnsiToHtml(this.props.content);

    return (
      <pre className="console-output" dangerouslySetInnerHTML={{__html: colored}}></pre>
    );
  }
});

module.exports = Console;
