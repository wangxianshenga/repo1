package com.pinyougou.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.*;

import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service(timeout = 5000)
public class ItemSearchServiceImpl implements ItemSearchService {

    @Autowired
    private SolrTemplate solrTemplate;

    @Override
    public Map search(Map searchMap) {
        Map map = new HashMap();

        //1.��ѯ�б�
        map.putAll(searchList(searchMap));
        //2.�����ѯ��Ʒ�����б�
        List<String> categoryList = searchCategoryList(searchMap);
        map.put("categoryList",categoryList);
        //3.��ѯƷ�ƺ͹���б�
        String category = (String) searchMap.get("category");
        if (!category.equals("")){
            map.putAll(searchBrandAndSpecList(category));
        }else {
            if (categoryList.size()>0){
                map.putAll(searchBrandAndSpecList(categoryList.get(0)));
            }
        }




        return map;
    }

    //��ѯ�б�
    private Map searchList(Map searchMap){
        Map map = new HashMap();
        //������ʾ��ʼ��
        HighlightQuery query = new SimpleHighlightQuery();
        HighlightOptions highlightOptions = new HighlightOptions().addField("item_title");//������
        highlightOptions.setSimplePrefix("<em style='color:red'>");//ǰ׺
        highlightOptions.setSimplePostfix("</em>");
        query.setHighlightOptions(highlightOptions);//Ϊ��ѯ�������ø���ѡ��

        //1.1�ؼ��ֲ�ѯ
        Criteria criteria =new Criteria("item_keywords").is(searchMap.get("keywords"));
        query.addCriteria(criteria);

        //1.2����Ʒ�������
        if(!"".equals(searchMap.get("category"))){//����û�ѡ���˷���
            FilterQuery filterQuery = new SimpleFilterQuery();
            Criteria filterCriteria = new Criteria("item_category").is(searchMap.get("category"));
            filterQuery.addCriteria(filterCriteria);
            query.addFilterQuery(filterQuery);
        }
        //1.3��Ʒ�ƹ���
        if(!"".equals(searchMap.get("brand"))){//����û�ѡ����Ʒ��
            FilterQuery filterQuery = new SimpleFilterQuery();
            Criteria filterCriteria = new Criteria("item_brand").is(searchMap.get("brand"));
            filterQuery.addCriteria(filterCriteria);
            query.addFilterQuery(filterQuery);
        }
        //1.4��������
        if (searchMap.get("spec")!=null){
            Map<String,String> specMap = (Map<String, String>) searchMap.get("spec");
            for(String key:specMap.keySet()){
                FilterQuery filterQuery = new SimpleFilterQuery();
                Criteria filterCriteria = new Criteria("item_spec_"+key).is(specMap.get(key));
                filterQuery.addCriteria(filterCriteria);
                query.addFilterQuery(filterQuery);
            }
        }


        //����ҳ����
        HighlightPage<TbItem> page = solrTemplate.queryForHighlightPage(query, TbItem.class);
        //������ڼ���
        List<HighlightEntry<TbItem>> entryList = page.getHighlighted();
        for(HighlightEntry<TbItem> entry:entryList){
            //��ȡ�����б�
            List<HighlightEntry.Highlight> highlightList = entry.getHighlights();
            /*for(HighlightEntry.Highlight h:highlightList){
                List<String> sns = h.getSnipplets();
                System.out.println(sns);
            }*/
            if(highlightList.size()>0 && highlightList.get(0).getSnipplets().size()>0){
                TbItem item = entry.getEntity();
                item.setTitle(highlightList.get(0).getSnipplets().get(0));
            }
        }
        map.put("rows",page.getContent());
        return map;
    }

    /**
     * �����ѯ����ѯ��Ʒ�����б�
     * @return
     */
    private List<String> searchCategoryList(Map searchMap){
        List<String> list = new ArrayList();

        Query query = new SimpleQuery("*:*");
        //���ݹؼ��ֲ�ѯ where
        Criteria criteria =new Criteria("item_keywords").is(searchMap.get("keywords"));
        query.addCriteria(criteria);
        //���÷���ѡ��
        GroupOptions groupOptions = new GroupOptions().addGroupByField("item_category");//group by
        query.setGroupOptions(groupOptions);
        //��ȡ����ҳ
        GroupPage<TbItem> page = solrTemplate.queryForGroupPage(query, TbItem.class);
        //��ȡ����������
        GroupResult<TbItem> groupResult = page.getGroupResult("item_category");
        //��ȡ�������ҳ
        Page<GroupEntry<TbItem>> groupEntries = groupResult.getGroupEntries();
        //��ȡ������ڼ���
        List<GroupEntry<TbItem>> entryList = groupEntries.getContent();

        for(GroupEntry<TbItem> entry:entryList){
            list.add(entry.getGroupValue());//������Ľ����ӵ�����ֵ��
        }
        return  list;

    }


    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * ������Ʒ�������Ʋ�ѯƷ�ƺ͹���б�
     * @param category  ��Ʒ��������
     * @return
     */
    private Map searchBrandAndSpecList(String category){
        Map map = new HashMap();

        //������Ʒ�������Ƶõ�ģ��id
        Long templateId = (Long) redisTemplate.boundHashOps("itemCat").get(category);
        if (templateId!=null){
            //����ģ��id��ȡƷ���б�
            List brandList = (List) redisTemplate.boundHashOps("brandList").get(templateId);
            map.put("brandList",brandList);
            //����ģ��id��ȡ����б�
            List specList = (List) redisTemplate.boundHashOps("specList").get(templateId);
            map.put("specList",specList);
        }

        return map;
    }

}
