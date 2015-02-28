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

  componentWillMount : function () {
    this.onUserStateChanged();
  },

  componentDidMount: function () {
    this.listenTo(UserStateStore, this.onUserStateChanged);
  },

  onUserStateChanged: function () {
    this.setState({
      username: UserStateStore.username
    });
  },

  render: function () {
    var loginLogout;
    if (!this.state.username) {
      loginLogout = <NavItemLink to="login">Login</NavItemLink>
    } else {
      loginLogout = <NavItem onClick={Actions.Logout}>Logout ({this.state.username})</NavItem>
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
