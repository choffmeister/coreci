// See http://en.wikipedia.org/wiki/ANSI_escape_code

var React = require('react');

var regex = /((?:\u001b\[)|\u009b)(?:((?:[0-9]{1,3})?(?:(?:;[0-9]{0,3})*)?)([A-M|f-m]))|(\u001b[A-M])/g;
var colorTable = ['black', 'red', 'green', 'yellow', 'blue', 'magenta', 'cyan', 'white'];

var convertColorsAnsiToHtml = function (raw) {
  var fgColor = null;
  var bgColor = null;

  var inner = raw.replace(regex, function (csi, a, b, c, d) {
    if (a == '\u001b\[' && c == 'm') {
      b.split(';').map(n => parseInt(n, 10)).forEach(number => {
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
    } else if (a == '\u001b\[' && ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'f'].indexOf(c) >= 0) {
      return '\n';
    } else {
      return '';
    }
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
