var React = require('react'),
    Link = require('react-router').Link,
    HttpClient = require('../services/HttpClient');

var jsonClient = new HttpClient.Json();

var ProjectsList = React.createClass({
  statics: {
    fetchData: function () {
      return {
        projects: jsonClient.get('/api/projects')
      };
    }
  },

  render: function () {
    var projects = this.props.data.projects.map(project => (
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

module.exports = ProjectsList;
