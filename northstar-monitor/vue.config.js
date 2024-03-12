module.exports = {
  // 选项...
  productionSourceMap: false,
  configureWebpack: {
    externals: {
      vue: 'Vue',
      'element-ui': 'ELEMENT'
    }
  },
  devServer: {
	  port: 8090,
    proxy: {
      '/version': {
        target: `http://localhost`
      },
      '/northstar': {
        target: `http://localhost`
      },
      '/redirect*': {
        target: `http://localhost`,
      },
    }
  }
}
