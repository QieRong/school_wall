package com.example.controller;

import com.example.common.Result;
import com.example.dto.HotwordStatsVO;
import com.example.dto.HotwordVO;
import com.example.service.HotwordService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/hotword")
public class HotwordAdminController {

  @Resource
  private HotwordService hotwordService;

  /**
   * 管理员获取热词列表
   */
  @GetMapping("/list")
  public Result<PageInfo<HotwordVO>> list(@RequestParam(required = false) Integer status,
      @RequestParam(defaultValue = "1") Integer pageNum,
      @RequestParam(defaultValue = "20") Integer pageSize) {
    return Result.success(hotwordService.getAdminList(status, pageNum, pageSize));
  }

  /**
   * 管理员删除热词
   */
  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Long id) {
    hotwordService.adminDelete(id);
    return Result.success(null);
  }

  /**
   * 设置官方推荐
   */
  @PostMapping("/{id}/recommend")
  public Result<Void> recommend(@PathVariable Long id,
      @RequestParam Boolean recommend) {
    hotwordService.setRecommend(id, recommend);
    return Result.success(null);
  }

  /**
   * 获取统计数据
   */
  @GetMapping("/stats")
  public Result<HotwordStatsVO> stats() {
    return Result.success(hotwordService.getStats());
  }

  /**
   * 获取异常投票预警列表
   * 
   * @param minVotes 最小投票次数阈值（默认10）
   * @param hours    时间范围小时数（默认24）
   */
  @GetMapping("/abnormal-voters")
  public Result<?> abnormalVoters(@RequestParam(defaultValue = "10") Integer minVotes,
      @RequestParam(defaultValue = "24") Integer hours) {
    return Result.success(hotwordService.getAbnormalVoters(minVotes, hours));
  }
}
