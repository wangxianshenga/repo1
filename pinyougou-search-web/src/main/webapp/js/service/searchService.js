//ËÑË÷·þÎñ²ã
app.service("searchService",function($http){
    this.search=function(searchMap){
        return $http.post('itemsearch/search.do',searchMap);
    }
});