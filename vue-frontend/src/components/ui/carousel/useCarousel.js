import { provide, inject, ref } from "vue";
import emblaCarouselVue from "embla-carousel-vue";

// 定义唯一的 Injection Key
const CarouselSymbol = Symbol("Carousel");

export function useProvideCarousel(props) {
  // 初始化 Embla Carousel
  const [emblaNode, emblaApi] = emblaCarouselVue(
    {
      ...props.opts,
      axis: props.orientation === "horizontal" ? "x" : "y",
    },
    props.plugins
  );

  // 状态
  const canScrollPrev = ref(false);
  const canScrollNext = ref(false);
  const scrollPrev = () => emblaApi.value?.scrollPrev();
  const scrollNext = () => emblaApi.value?.scrollNext();

  // 监听滚动事件以更新按钮状态
  const onSelect = (api) => {
    canScrollPrev.value = api.canScrollPrev();
    canScrollNext.value = api.canScrollNext();
  };

  // 挂载监听器
  if (emblaApi.value) {
    emblaApi.value.on("select", onSelect);
    emblaApi.value.on("reInit", onSelect);
  }

  // 将数据打包提供给子组件
  const carouselContext = {
    carouselRef: emblaNode,
    api: emblaApi,
    scrollPrev,
    scrollNext,
    canScrollPrev,
    canScrollNext,
    orientation: props.orientation,
  };

  provide(CarouselSymbol, carouselContext);

  return carouselContext;
}

export function useCarousel() {
  const context = inject(CarouselSymbol);
  if (!context) {
    throw new Error("useCarousel must be used within a <Carousel /> component.");
  }
  return context;
}
