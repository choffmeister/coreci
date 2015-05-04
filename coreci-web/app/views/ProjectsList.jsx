var React = require('react'),
    Link = require('react-router').Link,
    DateTime = require('../components/DateTime.jsx'),
    JsonClient = require('../services/HttpClient').Json();

var ProjectsList = React.createClass({
  statics: {
    fetchData: function () {
      return {
        projects: JsonClient.get('/api/projects')
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
