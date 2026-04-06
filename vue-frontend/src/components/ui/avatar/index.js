import { cva } from 'class-variance-authority'

export { default as Avatar } from './Avatar.vue'
export { default as AvatarImage } from './AvatarImage.vue'
export { default as AvatarFallback } from './AvatarFallback.vue'

export const avatarVariant = cva(
  'relative flex h-10 w-10 shrink-0 overflow-hidden rounded-full',
  {
    variants: {
      size: {
        sm: 'h-10 w-10',
        base: 'h-16 w-16',
        lg: 'h-32 w-32',
      },
      shape: {
        circle: 'rounded-full',
        square: 'rounded-md',
      },
    },
  },
)