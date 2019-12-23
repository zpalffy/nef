load('classpath:lunr-2.3.8.js')

var index = function (documents, pretty) {
    return JSON.stringify(lunr(function () {
        this.ref('slug')
        this.field('title')
        this.field('contents')
    
        var index = this; // so we can use the 'this' ref in the loop
        documents.forEach(function(doc) { 
            index.add(doc) 
        })
    }), null, pretty ? 2 : 0)
}
