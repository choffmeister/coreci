var React = require('react');

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


var Navigation = React.createClass({
  render: function () {
    return (
      <Navbar brand={this.props.brand}>
        <Nav>
          <NavItemLink to="builds-list">Builds</NavItemLink>
          <NavItemLink to="jobs-list">Jobs</NavItemLink>
          <NavItemLink to="about">About</NavItemLink>
          <NavItemLink to="login">Login</NavItemLink>
        </Nav>
      </Navbar>
    );
  }
});

module.exports = Navigation;
