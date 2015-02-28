var React = require('react'),
    Reflux = require('reflux');

var ReactRouter = require('react-router'),
    RouteHandler = ReactRouter.RouteHandler;

var ReactBootstrap = require('react-bootstrap'),
    Navbar = ReactBootstrap.Navbar,
    Nav = ReactBootstrap.Nav,
    NavItem = ReactBootstrap.NavItem,
    DropdownButton = ReactBootstrap.DropdownButton,
    MenuItem = ReactBootstrap.MenuItem;

var ReactRouterBootstrap = require('react-router-bootstrap'),
    NavItemLink = ReactRouterBootstrap.NavItemLink,
    MenuItemLink = ReactRouterBootstrap.MenuItemLink;

var Actions = require('../stores/Actions'),
    UserStateStore = require('../stores/UserStateStore');

var Navigation = React.createClass({
  mixins: [Reflux.ListenerMixin],

  getInitialState: function () {
    return {
      user: UserStateStore.user
    }
  },

  componentDidMount: function () {
    this.listenTo(UserStateStore, this.onUserStateChanged);
  },

  login: function () {
    Actions.Login('user1');
  },

  logout: function () {
    Actions.Logout();
  },

  onUserStateChanged: function (user) {
    this.setState({
      user: user
    });
  },

  render: function () {
    var loginLogout;
    if (!this.state.user) {
      loginLogout = <NavItem onClick={this.login}>Login</NavItem>
    } else {
      loginLogout = <NavItem onClick={this.logout}>Logout ({this.state.user})</NavItem>
    }
    return (
      <Navbar brand={this.props.brand}>
        <Nav>
          <NavItemLink to="builds-list">Builds</NavItemLink>
          <NavItemLink to="jobs-list">Jobs</NavItemLink>
          <NavItemLink to="about">About</NavItemLink>
        </Nav>
        <Nav right={true}>
          {loginLogout}
        </Nav>
      </Navbar>
    );
  }
});

module.exports = Navigation;
