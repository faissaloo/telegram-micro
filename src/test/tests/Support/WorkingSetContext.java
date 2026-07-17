package test;

import support.WorkingSet;

public class WorkingSetContext {
  public static class PlanSeparatesRequiredAndOptionalBytesTest extends Test {
    public String label() {
      return "Working set plans separate required and optional bytes";
    }

    public void test() throws TestFailureException {
      WorkingSet workingSet = new WorkingSet();
      WorkingSet.Plan plan;

      workingSet.init(1024, 128);
      workingSet.add(WorkingSet.WORK_AUTH, 256, true);
      workingSet.add(WorkingSet.WORK_UI, 128, true);
      workingSet.add(WorkingSet.WORK_READER_HISTORY, 512, false);
      plan = workingSet.plan();

      expect(plan.availableBytes, 896);
      expect(plan.requiredBytes, 384);
      expect(plan.optionalBytes, 512);
      expect(plan.fitsInRam, true);
      expect(plan.optionalFits, true);
    }
  }
}
