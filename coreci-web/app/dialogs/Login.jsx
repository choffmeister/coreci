var React = require('react'),
    ReactBootstrap = require('react-bootstrap'),
    Button = ReactBootstrap.Button,
    Modal = ReactBootstrap.Modal,
    AccessToken = require('../services/AccessToken'),
    Callout = require('../components/Callout.jsx');

var Login = React.createClass({
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

    AccessToken.create(this.state.username, this.state.password)
      .then(token => {
        this.props.onRequestHide();
      })
      .catch(err => {
        this.reset();
        this.setState({
          message: {
            type: err === null ? 'warning' : 'error',
            text: err === null ? 'The credentials are invalid.' : 'There was an unknown error.'
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
      <Modal {...this.props} bsStyle="default" title="Login" animation={false} backdrop={true}>
        <form onSubmit={this.onSubmit} autoComplete="off">
          <div className="modal-body">
            {message}
            <div className="form-group">
              <label htmlFor="username">Username</label>
              <input onChange={this.onChange} value={this.state.username} disabled={this.state.busy} ref="username" type="text" id="username" placeholder="Your username" className="form-control"/>
            </div>
            <div className="form-group">
              <label htmlFor="password">Password</label>
              <input onChange={this.onChange} value={this.state.password} disabled={this.state.busy} ref="password" type="password" id="password" placeholder="Your password" className="form-control"/>
            </div>
          </div>
          <div className="modal-footer">
            <Button onClick={this.onSubmit} type="submit">Login</Button>
            <Button onClick={this.props.onRequestHide}>Cancel</Button>
          </div>
        </form>
      </Modal>
    );
  }
});

module.exports = Login;
