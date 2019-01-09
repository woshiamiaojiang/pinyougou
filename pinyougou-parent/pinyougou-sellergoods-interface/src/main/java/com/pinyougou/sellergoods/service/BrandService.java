package com.pinyougou.sellergoods.service;

import com.pinyougou.pojo.TbBrand;
import entity.PageResult;

import java.util.List;
import java.util.Map;

/**
 * 品牌接口
 * @author Administrator
 *
 */
public interface BrandService {

	List<TbBrand> findAll();

	/**
	 * 返回分页列表
	 * @return
	 */
	public PageResult findPage(int pageNum,int pageSize);

	/**
	 * 增加
	 */
	void add(TbBrand brand);

	/**
	 * 根据ID查询实体
	 * @param id
	 * @return
	 */
    TbBrand findOne(Long id);

	/**
     * 修改
     * @param brand
     */
    void update(TbBrand brand);

    /**
     * 删除
     * @param ids
     */
    void delete(Long[] ids);

    /**
     * 品牌分页
     * @param pageNum 当前页面
     * @param pageSize 每页记录数
     * @return
     */
    PageResult findPage(TbBrand brand, int pageNum,int pageSize);

	/**
	 * 返回下拉列表数据
	 * @return
	 */
	List<Map> selectOptionList();
}
