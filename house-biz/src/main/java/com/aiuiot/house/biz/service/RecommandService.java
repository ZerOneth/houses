package com.aiuiot.house.biz.service;

import com.aiuiot.house.common.model.House;
import com.aiuiot.house.common.page.PageParams;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 热门房产
 * @Author aiuiot
 * @Date 2019/11/12 9:39 下午
 **/
@Service
public class RecommandService {
    private static final String HOT_HOUSE_KEY = "hot_house";

    @Autowired
    private HouseService houseService;

    public void increase(Long id){
        Jedis jedis = new Jedis("127.0.0.1");
        jedis.zincrby(HOT_HOUSE_KEY, 1.0, id+""); //1.0 为增加分数 zincrby热度
        jedis.zremrangeByRank(HOT_HOUSE_KEY, 10, -1);//排序之后删除10个以外的元素列表
        jedis.close();
    }

    public List<Long> getHot(){
        Jedis jedis = new Jedis("127.0.0.1");
        Set<String> idSet = jedis.zrevrange(HOT_HOUSE_KEY, 0, -1);
        List<Long> ids = idSet.stream().map(Long::parseLong).collect(Collectors.toList());
        return ids;
    }

    public List<House> getHotHouse(Integer size){
        House query = new House();
        List<Long> list = getHot();
        list = list.subList(0, Math.min(list.size(), size));
        if (list.isEmpty()){
            return Lists.newArrayList();
        }
        query.setIds(list);
        List<House> houses = houseService.queryAndSetImg(query, PageParams.build(size, 1));
        final List<Long> orderList = list;
        Ordering<House> houseSort = Ordering.natural().onResultOf(hs ->{
           return orderList.indexOf(hs.getId());
        });
        return houseSort.sortedCopy(houses);
    }
}
