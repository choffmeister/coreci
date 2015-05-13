var React = require('react'),
    ReactAddons = require('react/addons').addons,
    Button = ReactBootstrap.Button,
    Input = require('react-bootstrap').Input,
    JsonClient = require('../services/HttpClient').Json();

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
  mixins: [ReactAddons.LinkedStateMixin],

  contextTypes: {
    router: React.PropTypes.func
  },

  getInitialState: function () {
    return {
      title: 'My project',
      description: '',
      image: 'busybox:latest',
      script: '#!/bin/sh -e\n\necho hello world\n\nexit 0\n'
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
      id: '000000000000000000000000',
      userId: '000000000000000000000000',
      nextBuildNumber: 1,
      createdAt: 0,
      updatedAt: 0
    };

    JsonClient.post('/api/projects', project).then(project => {
      this.context.router.transitionTo('projects-show', { projectCanonicalName: project.canonicalName });
    });
  },

  render: function () {
    return (
      <div>
        <h1>Create project</h1>
        <form onSubmit={this.onSubmit}>
          <Input type="text" label="Title" valueLink={this.linkState('title')}/>
          <Input type="textarea" label="Description" valueLink={this.linkState('description')} rows="6"/>
          <Input type="text" label="Image" valueLink={this.linkState('image')}/>
          <Input type="textarea" label="Script" valueLink={this.linkState('script')} rows="10"/>
          <Button onClick={this.onSubmit} type="submit">Create</Button>
        </form>
      </div>
    );
  }
});

module.exports = ProjectsCreate;
