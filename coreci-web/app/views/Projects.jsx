var React = require('react'),
    Link = require('react-router').Link,
    RestClient = require('../services/RestClient'),
    DateTime = require('../components/DateTime.jsx');

var Projects = React.createClass({
  statics: {
    fetchData: function () {
      return {
        projects: RestClient.get('/api/projects')
      };
    }
  },

  render: function () {
    var projects = this.props.data['projects-list'].projects.map(project => (
      <tr key={project.id}>
        <td><Link to="projects-show" params={{projectCanonicalName: project.canonicalName}}>{project.canonicalName}</Link></td>
      </tr>
    ));
    return (
      <table className="table">
        <thead>
          <tr>
            <th>project</th>
          </tr>
        </thead>
        <tbody>
          {projects}
        </tbody>
      </table>
    );
  }
});

module.exports = Projects;
