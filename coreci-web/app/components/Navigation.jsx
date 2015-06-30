var React = require('react'),
    ReactBootstrap = require('react-bootstrap'),
    ReactRouterBootstrap = require('react-router-bootstrap');

var Navbar = ReactBootstrap.Navbar,
    Nav = ReactBootstrap.Nav,
    CollapsableNav = ReactBootstrap.CollapsableNav,
    NavItem = ReactBootstrap.NavItem,
    ModalTrigger = ReactBootstrap.ModalTrigger,
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
          <Nav navbar left>
            <NavItemLink to="builds-list" eventKey={1}>Builds</NavItemLink>
            <NavItemLink to="projects-list" eventKey={2}>Projects</NavItemLink>
            <NavItemLink to="workers-list" eventKey={2}>Workers</NavItemLink>
          </Nav>
          <Nav navbar right>
            <NavItemLink to="projects-create" eventKey={1}>Create</NavItemLink>
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
