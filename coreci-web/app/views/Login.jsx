var React = require('react'),
    ReactRouter = require('react-router'),
    Actions = require('../stores/Actions'),
    Callout = require('../components/Callout.jsx');

var Login = React.createClass({
  mixins: [ReactRouter.Navigation],

  getInitialState: function () {
    return {
      username: '',
      password: '',
      busy: false,
      message: null
    };
  },

  componentDidMount: function () {
    this.reset();
  },

  onSubmit: function (event) {
    event.preventDefault();
    this.setState({ busy: true });

    Actions.Login.triggerPromise(this.state.username, this.state.password)
      .then(user => {
        if (user) {
          this.transitionTo('app');
        } else {
          this.reset();
          this.setState({
            message: {
              type: 'warning',
              text: 'The credentials are invalid.'
            }
          });
        }
      })
      .catch(error => {
        console.error('err', error);
        this.reset();
        this.setState({
          message: {
            type: 'error',
            text: 'There was an unknown error.'
          }
        });
      });
  },

  onChange: function () {
    this.setState({
      username: this.refs.username.getDOMNode().value,
      password: this.refs.password.getDOMNode().value
    })
  },

  reset: function () {
    this.setState({
      username: '',
      password: '',
      busy: false
    });
    this.refs.username.getDOMNode().focus();
  },

  render: function () {
    var message;
    if (this.state.message) {
      message = <Callout kind={this.state.message.type}>{this.state.message.text}</Callout>;
    }
    return (
      <form onSubmit={this.onSubmit}>
        {message}
        <div className="form-group">
          <label htmlFor="username">Username</label>
          <input onChange={this.onChange} value={this.state.username} disabled={this.state.busy} ref="username" type="text" id="username" placeholder="Your username" className="form-control"/>
        </div>
        <div className="form-group">
          <label htmlFor="password">Password</label>
          <input onChange={this.onChange} value={this.state.password} disabled={this.state.busy} ref="password" type="password" id="password" placeholder="Your password" className="form-control"/>
        </div>
        <button type="submit" disabled={this.state.busy} className="btn btn-primary">Login</button>
      </form>
    );
  }
});

module.exports = Login;
