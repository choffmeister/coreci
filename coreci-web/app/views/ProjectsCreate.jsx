var React = require('react'),
    ReactAddons = require('react/addons'),
    ReactBootstrap = require('react-bootstrap'),
    HttpClient = require('../services/HttpClient');

var jsonClient = new HttpClient.Json();

var slugify = function (text) {
  return text
    .toLowerCase()
    .replace(/\s/g, '-')
    .replace(/[^a-z0-9]/g, '-')
    .replace(/\-{2,}/g, '-')
    .replace(/^\-+/, '')
    .replace(/\-+$/, '');
};

var ProjectsCreate = React.createClass({
  mixins: [ReactAddons.addons.LinkedStateMixin],

  contextTypes: {
    router: React.PropTypes.func
  },

  getInitialState: function () {
    return {
      title: 'My project',
      description: '',
      image: 'busybox:latest',
      script: '#!/bin/sh -e\n\necho hello world\n\nexit 0\n',
      environment: 'KEY=value'
    };
  },

  onSubmit: function (event) {
    event.preventDefault();

    var project = {
      title: this.state.title,
      canonicalName: slugify(this.state.title),
      description: this.state.description,
      image: this.state.image,
      script: this.state.script,
      environment: this.state.environment.split(/\r\n|\n/).map(l => {
        var match = l.match(/^\s*([a-zA-Z0-9_]+)\s*=\s*(.*)\s*$/);
        if (match) {
          return {
            name: match[1].trim(),
            value: match[2].trim(),
            secret: false
          };
        } else {
          return null;
        }
      }).filter(ev => ev !== null),
      id: '000000000000000000000000',
      userId: '000000000000000000000000',
      nextBuildNumber: 1,
      createdAt: 0,
      updatedAt: 0
    };

    jsonClient.post('/api/projects', project).then(createdProject => {
      this.context.router.transitionTo('projects-show', { projectCanonicalName: createdProject.canonicalName });
    });
  },

  render: function () {
    return (
      <div>
        <h1>Create project</h1>
        <form onSubmit={this.onSubmit}>
          <ReactBootstrap.Input type="text" label="Title" valueLink={this.linkState('title')}/>
          <ReactBootstrap.Input type="textarea" label="Description" valueLink={this.linkState('description')} rows="6"/>
          <ReactBootstrap.Input type="text" label="Image" valueLink={this.linkState('image')}/>
          <ReactBootstrap.Input type="textarea" label="Script" valueLink={this.linkState('script')} rows="10"/>
          <ReactBootstrap.Input type="textarea" label="Environment" valueLink={this.linkState('environment')} rows="4"/>
          <ReactBootstrap.Button onClick={this.onSubmit} type="submit">Create</ReactBootstrap.Button>
        </form>
      </div>
    );
  }
});

module.exports = ProjectsCreate;
