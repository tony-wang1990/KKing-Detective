    /**
     * Show retry interval selection options
     */
    private BotApiMethod<? extends Serializable> showIntervalOptions(
            CallbackQuery callbackQuery,
            String userId,
            String planType,
            int count) {
        
        log.info("showIntervalOptions: userId={}, planType={}, count={}", userId, planType, count);
        
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        OciUser user = userService.getById(userId);
        
        if (user == null) {
            log.warn("showIntervalOptions: User not found: userId={}", userId);
            return buildEditMessage(
                    callbackQuery,
                    "❌ 配置不存在",
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
        
        log.debug("showIntervalOptions: User found: username={}", user.getUsername());
        
        // 获取方案详情
        InstancePlan plan = getPlanByType(planType);
        
        // Get plan display name
        String planName = switch (planType) {
            case "plan1" -> "方案1";
            case "plan2" -> "方案2";
            case "plan3" -> "方案3";
            case "plan4" -> "方案4";
            default -> planType;
        };
        
        // Build message asking about retry interval
        String message = String.format(
                "【选择抢机间隔】\n\n" +
                "🔑 配置名：%s\n" +
                "🌏 区域：%s\n" +
                "💻 方案：%s\n" +
                "⚙️ 配置：%dC%dG%dG\n" +
                "🏗️ 架构：%s\n" +
                "💿 系统：%s\n" +
                "🔢 数量：%d台\n\n" +
                "⏱️ 请选择抢机重试间隔：\n" +
                "（间隔越短，成功率越高，但消耗资源也越多）",
                user.getUsername(),
                user.getOciRegion(),
                planName,
                plan.getOcpus(),
                plan.getMemory(),
                plan.getDisk(),
                plan.getArchitecture(),
                plan.getOperationSystem(),
                count
        );
        
        // Build keyboard with interval options
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button(
                        "⚡ 30秒 (激进)",
                        "create_instance:" + userId + ":" + planType + ":" + count + ":30"
                ),
                KeyboardBuilder.button(
                        "🚀 60秒 (推荐)",
                        "create_instance:" + userId + ":" + planType + ":" + count + ":60"
                )
        ));
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button(
                        "⏰ 80秒 (默认)",
                        "create_instance:" + userId + ":" + planType + ":" + count + ":80"
                ),
                KeyboardBuilder.button(
                        "🐌 120秒 (保守)",
                        "create_instance:" + userId + ":" + planType + ":" + count + ":120"
                )
        ));
        
        keyboard.add(new InlineKeyboardRow(
                KeyboardBuilder.button("◀️ 返回", "create_instance:" + userId + ":" + planType)
        ));
        keyboard.add(KeyboardBuilder.buildCancelRow());
        
        log.debug("showIntervalOptions: Built message with {} keyboard rows", keyboard.size());
        EditMessageText result = buildEditMessage(
                callbackQuery,
                message,
                new InlineKeyboardMarkup(keyboard)
        );
        log.info("showIntervalOptions: Successfully built EditMessageText for userId={}, count={}", userId, count);
        return result;
    }
