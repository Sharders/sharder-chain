'use strict'
const path = require('path')
const _ = (module.exports = {})

_.cwd = file => {
  return path.join(process.cwd(), file || '')
}

_.outputPath = path.join(__dirname, '../../html/www')

_.outputIndexPath = path.join(__dirname, '../../html/www/index.html')

_.target = 'web'