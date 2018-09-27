app.controller('searchController',function($scope,searchService){

    //������������Ľṹ
    $scope.searchMap={'keywords':'','category':'','brand':'','spec':{}};

    //����
    $scope.search=function(){
        searchService.search( $scope.searchMap ).success(
            function(response){
                $scope.resultMap=response;//�������صĽ��
            }
        );
    }


    //���������  �ı�searchMap��ֵ
    $scope.addSearchItem=function (key,value) {
        if(key=='category' || key=='brand'){//����û�������Ƿ����Ʒ��
            $scope.searchMap[key]=value;
        }else {//�û�������ǹ��
            $scope.searchMap.spec[key]=value;
        }
        $scope.search();//��ѯ
    }


    //����������
    $scope.removeSearchItem=function (key) {
        if(key=='category' || key=='brand'){//����û�������Ƿ����Ʒ��
            $scope.searchMap[key]="";
        }else {//�û�������ǹ��
            delete $scope.searchMap.spec[key];
        }
        $scope.search();//��ѯ
    }



});