var React = require('react');
    ReactRouter = require('react-router'),
    RouteHandler = ReactRouter.RouteHandler,
    ReactBootstrap = require('react-bootstrap'),
    Navbar = ReactBootstrap.Navbar,
    Nav = ReactBootstrap.Nav,
    NavItem = ReactBootstrap.NavItem,
    ModalTrigger = ReactBootstrap.ModalTrigger,
    ReactRouterBootstrap = require('react-router-bootstrap'),
    NavItemLink = ReactRouterBootstrap.NavItemLink;

var LoginDialog = require('../dialogs/Login.jsx');

var Navigation = React.createClass({
  noop: function (event) {
    event.preventDefault();
  },

  render: function () {
    return (
      <Navbar brand={this.props.brand}>
        <Nav>
          <NavItemLink to="builds-list">Builds</NavItemLink>
          <NavItemLink to="jobs-list">Jobs</NavItemLink>
          <ModalTrigger modal={<LoginDialog />}>
            <NavItem onClick={this.noop}>Login</NavItem>
          </ModalTrigger>
        </Nav>
      </Navbar>
    );
  }
});

module.exports = Navigation;
