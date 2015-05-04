var React = require('react'),
    ReactAddons = require('react/addons').addons,
    Button = ReactBootstrap.Button,
    Input = require('react-bootstrap').Input,
    JsonClient = require('../services/HttpClient').Json();

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
      command: '["uname", "-a"]'
    };
  },

  onSubmit: function () {
    var slugify = function (text) {
      return text
        .toLowerCase()
        .replace(/\s/g, '-')
        .replace(/[^a-z0-9]/g, '-')
        .replace(/\-{2,}/g, '-')
        .replace(/^\-+/, '')
        .replace(/\-+$/, '');
    };

    var project = {
      title: this.state.title,
      canonicalName: slugify(this.state.title),
      description: this.state.description,
      image: this.state.image,
      command: JSON.parse(this.state.command),
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
        <form>
          <Input type="text" label="Title" valueLink={this.linkState('title')}/>
          <Input type="textarea" label="Description" valueLink={this.linkState('description')} rows="6"/>
          <Input type="text" label="Image" valueLink={this.linkState('image')}/>
          <Input type="text" label="Command" valueLink={this.linkState('command')}/>
          <Button onClick={this.onSubmit} type="submit">Create</Button>
        </form>
      </div>
    );
  }
});

module.exports = ProjectsCreate;
