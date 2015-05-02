var React = require('react');
    ReactRouter = require('react-router'),
    RouteHandler = ReactRouter.RouteHandler,
    ReactBootstrap = require('react-bootstrap'),
    Navbar = ReactBootstrap.Navbar,
    Nav = ReactBootstrap.Nav,
    CollapsableNav = ReactBootstrap.CollapsableNav,
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
      <Navbar brand={this.props.brand} toggleNavKey={0}>
        <CollapsableNav eventKey={0}>
          <Nav navbar right>
            <NavItemLink to="builds-list" eventKey={1}>Builds</NavItemLink>
            <NavItemLink to="projects-list" eventKey={2}>Projects</NavItemLink>
            <NavItemLink to="workers-list" eventKey={2}>Workers</NavItemLink>
            <ModalTrigger modal={<LoginDialog />}>
              <NavItem onClick={this.noop} eventKey={3}>Login</NavItem>
            </ModalTrigger>
          </Nav>
        </CollapsableNav>
      </Navbar>
    );
  }
});

module.exports = Navigation;
