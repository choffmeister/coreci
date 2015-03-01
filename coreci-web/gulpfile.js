var argv = require('yargs').argv,
    browserify = require('browserify'),
    buffer = require('vinyl-buffer'),
    connect = require('connect'),
    gif = require('gulp-if'),
    gulp = require('gulp'),
    gutil = require('gulp-util'),
    less = require('gulp-less'),
    livereload = require('gulp-livereload'),
    minifyhtml = require('gulp-minify-html'),
    path = require('path'),
    proxy = require('proxy-middleware'),
    reactify = require('reactify'),
    rewrite = require('connect-modrewrite'),
    size = require('gulp-size'),
    source = require('vinyl-source-stream'),
    sourcemaps = require('gulp-sourcemaps'),
    uglify = require('gulp-uglify'),
    url = require('url');

var config = {
  debug: !argv.dist,
  dist: argv.dist,
  port: argv.port || 9000,
  dest: function (p) {
    return path.join(argv.target || './target', p || '');
  }
};

var onerror = function (err) {
  if (config.debug) {
    gutil.beep();
    gutil.log(err.message);
    this.emit('end');
  } else {
    throw err;
  }
};

gulp.task('html', function () {
  return gulp.src('./app/index.html')
    .pipe(gif(config.dist, minifyhtml()))
    .pipe(size({ showFiles: true, gzip: config.dist }))
    .pipe(gulp.dest(config.dest()))
    .pipe(livereload({ auto: false }));
});

gulp.task('css', function () {
  return gulp.src('./app/style.less')
    .pipe(less({ compress: config.dist }))
    .on('error', onerror)
    .pipe(size({ showFiles: true, gzip: config.dist }))
    .pipe(gulp.dest(config.dest('app')))
    .pipe(livereload({ auto: false }));
});

gulp.task('javascript', function () {
  var bundler = browserify('./app/main.jsx')
    .transform(reactify);
  return bundle();

  function bundle() {
    return bundler.bundle()
      .on('error', onerror)
      .pipe(source('app.js'))
      .pipe(buffer())
      .pipe(gif(config.debug, sourcemaps.init({ localMaps: true })))
      .pipe(gif(config.dist, uglify({ preserveComments: 'some' })))
      .pipe(size({ showFiles: true, gzip: config.dist }))
      .pipe(gif(config.debug, sourcemaps.write('.')))
      .pipe(gulp.dest(config.dest('app')))
      .pipe(livereload({ auto: false }));
  }
});

gulp.task('assets-bootstrap-font', function () {
  return gulp.src('./node_modules/bootstrap/fonts/*.*')
    .pipe(gulp.dest(config.dest('app/fonts')));
});
gulp.task('assets', ['assets-bootstrap-font']);

gulp.task('connect', ['build'], function (next) {
  connect()
    .use('/api', proxy(url.parse('http://localhost:8080/api')))
    .use(rewrite(['!(^/app/) /index.html [L]']))
    .use(connect.static(config.dest()))
    .listen(config.port, next);
});

gulp.task('watch', ['build'], function () {
  livereload.listen({ auto: true });
  gulp.watch('./index.html', ['html']);
  gulp.watch('./app/**/*.{css,less}', ['css']);
  gulp.watch('./app/**/*.{js,jsx}', ['javascript']);
});

gulp.task('build', ['html', 'css', 'javascript', 'assets']);
gulp.task('server', ['connect', 'watch']);
gulp.task('default', ['server']);
